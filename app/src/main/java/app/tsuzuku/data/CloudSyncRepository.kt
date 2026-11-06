package app.tsuzuku.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import app.tsuzuku.BuildConfig
import app.tsuzuku.data.preferences.SettingsRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

sealed interface CloudAccountState {
    data object Unavailable : CloudAccountState
    data object SignedOut : CloudAccountState
    data class SignedIn(
        val displayName: String,
        val email: String,
        val photoUrl: String?,
    ) : CloudAccountState
}

sealed interface CloudSyncState {
    data object Idle : CloudSyncState
    data class Working(val operation: CloudOperation) : CloudSyncState
    data class ChoiceRequired(
        val local: BackupSummary,
        val cloud: BackupSummary?,
        val conflict: Boolean,
    ) : CloudSyncState
    data class Success(
        val operation: CloudOperation,
        val summary: BackupSummary,
        val syncedAt: Long,
    ) : CloudSyncState
    data class NoChanges(val syncedAt: Long) : CloudSyncState
    data class Error(val message: String) : CloudSyncState
}

enum class CloudOperation { SignIn, Upload, Download, Synchronize, SignOut, DeleteAccount }

/**
 * Keeps Room as the source of truth and stores versioned, chunked snapshots in Firestore.
 * A last-synced hash prevents one device from silently overwriting changes made on another.
 */
class CloudSyncRepository(
    private val context: Context,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val credentialManager = CredentialManager.create(context)
    private val configured = BuildConfig.FIREBASE_CONFIGURED && FirebaseApp.getApps(context).isNotEmpty()
    private val auth: FirebaseAuth? = if (configured) FirebaseAuth.getInstance() else null
    private val firestore: FirebaseFirestore? = if (configured) FirebaseFirestore.getInstance() else null

    private val mutableAccount = MutableStateFlow<CloudAccountState>(CloudAccountState.Unavailable)
    val account: StateFlow<CloudAccountState> = mutableAccount.asStateFlow()

    private val mutableSyncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = mutableSyncState.asStateFlow()

    init {
        if (!configured) {
            mutableAccount.value = CloudAccountState.Unavailable
        } else {
            auth?.addAuthStateListener { updateAccount(it) }
            auth?.let(::updateAccount)
        }
    }

    suspend fun signIn(activity: Activity): CloudAccountState.SignedIn? {
        val firebaseAuth = auth
        if (!configured || firebaseAuth == null) {
            mutableSyncState.value = CloudSyncState.Error("Firebase configuration is missing")
            return null
        }
        val clientId = serverClientId()
        if (clientId == null) {
            mutableSyncState.value = CloudSyncState.Error("Google OAuth client is missing")
            return null
        }
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.SignIn)
        return try {
            val googleOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val response = credentialManager.getCredential(
                context = activity,
                request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build(),
            )
            val credential = response.credential
            require(
                credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            ) { "Unsupported Google credential" }
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val result = firebaseAuth
                .signInWithCredential(GoogleAuthProvider.getCredential(googleCredential.idToken, null))
                .await()
            val user = requireNotNull(result.user)
            val signedIn = CloudAccountState.SignedIn(
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                photoUrl = user.photoUrl?.toString(),
            )
            mutableAccount.value = signedIn
            mutableSyncState.value = CloudSyncState.Idle
            signedIn
        } catch (error: NoCredentialException) {
            mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
            null
        } catch (error: Exception) {
            mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
            null
        }
    }

    /**
     * A first installation has no local history to protect, so an existing cloud copy can be
     * restored without presenting a blocking source-choice screen. If the account is new, the
     * empty local snapshot establishes its first cloud baseline.
     */
    suspend fun initializeFreshInstallation() {
        if (!settingsRepository.settings.value.cloudSyncEnabled) return
        val user = auth?.currentUser ?: return
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.Synchronize)
        runCatching { readCloudMetadata(user.uid) }
            .onSuccess { cloud ->
                if (cloud == null) uploadDevice() else downloadCloud()
            }
            .onFailure { error ->
                mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
            }
    }

    suspend fun signOut(removeLocalData: Boolean = false) {
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.SignOut)
        runCatching {
            auth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            if (removeLocalData) backupRepository.clearLocalData()
            settingsRepository.clearCloudSyncBaseline()
            mutableSyncState.value = CloudSyncState.Idle
        }.onFailure { error ->
            mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
        }
    }

    /**
     * Permanently deletes the user's cloud copy, their Firebase account and the data on this
     * device (required by Google Play for apps that let people create an account).
     * Returns false when Firebase asks for a fresh sign-in before deleting the account.
     */
    suspend fun deleteAccount(): Boolean {
        val user = auth?.currentUser ?: return false
        val database = firestore ?: return false
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.DeleteAccount)
        return runCatching {
            val root = userRoot(database, user.uid)
            root.collection(COLLECTION_GENERATIONS).get().await().documents.forEach { generation ->
                deleteGeneration(database, user.uid, generation.id)
            }
            root.collection(COLLECTION_SYNC).get().await().documents.forEach { it.reference.delete().await() }
            root.delete().await()
            user.delete().await()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            backupRepository.clearLocalData()
            settingsRepository.clearCloudSyncBaseline()
            settingsRepository.setCloudSyncEnabled(false)
            mutableSyncState.value = CloudSyncState.Idle
            true
        }.getOrElse { error ->
            mutableSyncState.value = CloudSyncState.Error(
                if (error is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    context.getString(app.tsuzuku.R.string.delete_account_reauth)
                } else {
                    error.safeMessage()
                },
            )
            false
        }
    }

    suspend fun prepareChoice(conflict: Boolean = false) {
        val user = auth?.currentUser ?: return
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.Synchronize)
        runCatching {
            val localJson = backupRepository.exportJson()
            val local = backupRepository.preview(localJson)
            val cloud = readCloudMetadata(user.uid)?.summary
            mutableSyncState.value = CloudSyncState.ChoiceRequired(local, cloud, conflict)
        }.onFailure { error ->
            mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
        }
    }

    suspend fun uploadDevice() {
        val user = auth?.currentUser ?: return notSignedIn()
        val database = firestore ?: return unavailable()
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.Upload)
        runCatching {
            val json = backupRepository.exportJson()
            val summary = backupRepository.preview(json)
            val payload = CloudChunkCodec.encode(json)
            val generationId = UUID.randomUUID().toString()
            val root = userRoot(database, user.uid)
            val pointer = root.collection(COLLECTION_SYNC).document(DOCUMENT_CURRENT)
            val oldGenerationId = pointer.get().await().getString(FIELD_GENERATION_ID)
            val generation = root.collection(COLLECTION_GENERATIONS).document(generationId)
            val batch = database.batch()
            batch.set(generation, generationMetadata(payload, summary))
            payload.chunks.forEachIndexed { index, bytes ->
                batch.set(
                    generation.collection(COLLECTION_CHUNKS).document(index.toString().padStart(4, '0')),
                    mapOf(FIELD_INDEX to index, FIELD_PAYLOAD to Blob.fromBytes(bytes)),
                )
            }
            batch.commit().await()
            pointer.set(
                mapOf(
                    FIELD_GENERATION_ID to generationId,
                    FIELD_HASH to payload.hash,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    FIELD_CLIENT_UPDATED_AT to System.currentTimeMillis(),
                ),
            ).await()
            settingsRepository.recordCloudSync(payload.hash)
            mutableSyncState.value = CloudSyncState.Success(
                CloudOperation.Upload,
                summary,
                settingsRepository.settings.value.lastCloudSyncAt,
            )
            if (!oldGenerationId.isNullOrBlank() && oldGenerationId != generationId) {
                runCatching { deleteGeneration(database, user.uid, oldGenerationId) }
            }
        }.onFailure { error ->
            mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
        }
    }

    suspend fun downloadCloud() {
        val user = auth?.currentUser ?: return notSignedIn()
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.Download)
        runCatching {
            val cloud = readCloudSnapshot(user.uid) ?: error("No cloud backup exists")
            val summary = backupRepository.restore(cloud.json)
            settingsRepository.recordCloudSync(cloud.hash)
            mutableSyncState.value = CloudSyncState.Success(
                CloudOperation.Download,
                summary,
                settingsRepository.settings.value.lastCloudSyncAt,
            )
        }.onFailure { error ->
            mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
        }
    }

    suspend fun synchronize() {
        if (!settingsRepository.settings.value.cloudSyncEnabled) return
        val user = auth?.currentUser ?: return
        mutableSyncState.value = CloudSyncState.Working(CloudOperation.Synchronize)
        runCatching {
            val localJson = backupRepository.exportJson()
            val localHash = CloudChunkCodec.hash(localJson.toByteArray(Charsets.UTF_8))
            val baseline = settingsRepository.settings.value.lastCloudHash
            val cloud = readCloudMetadata(user.uid)
            when {
                baseline.isBlank() -> prepareChoice()
                cloud == null -> uploadDevice()
                localHash == cloud.hash -> {
                    settingsRepository.recordCloudSync(localHash)
                    mutableSyncState.value = CloudSyncState.NoChanges(settingsRepository.settings.value.lastCloudSyncAt)
                }
                cloud.hash == baseline -> uploadDevice()
                localHash == baseline -> downloadCloud()
                else -> prepareChoice(conflict = true)
            }
        }.onFailure { error ->
            mutableSyncState.value = CloudSyncState.Error(error.safeMessage())
        }
    }

    fun clearStatus() {
        mutableSyncState.value = CloudSyncState.Idle
    }

    private suspend fun readCloudMetadata(uid: String): CloudMetadata? {
        val database = firestore ?: return null
        val root = userRoot(database, uid)
        val pointer = root.collection(COLLECTION_SYNC).document(DOCUMENT_CURRENT).get().await()
        val generationId = pointer.getString(FIELD_GENERATION_ID) ?: return null
        val generation = root.collection(COLLECTION_GENERATIONS).document(generationId).get().await()
        if (!generation.exists()) return null
        return CloudMetadata(
            generationId = generationId,
            hash = generation.getString(FIELD_HASH).orEmpty(),
            chunkCount = generation.getLong(FIELD_CHUNK_COUNT)?.toInt() ?: 0,
            summary = BackupSummary(
                habitCount = generation.getLong(FIELD_HABIT_COUNT)?.toInt() ?: 0,
                historyCount = generation.getLong(FIELD_HISTORY_COUNT)?.toInt() ?: 0,
                contentCount = generation.getLong(FIELD_CONTENT_COUNT)?.toInt() ?: 0,
                goalCount = generation.getLong(FIELD_GOAL_COUNT)?.toInt() ?: 0,
            ),
        ).also {
            require(it.hash.isNotBlank() && it.chunkCount in 1..CloudChunkCodec.MAX_CHUNKS) {
                "Invalid cloud backup metadata"
            }
        }
    }

    private suspend fun readCloudSnapshot(uid: String): CloudSnapshot? {
        val database = firestore ?: return null
        val metadata = readCloudMetadata(uid) ?: return null
        val generation = userRoot(database, uid)
            .collection(COLLECTION_GENERATIONS)
            .document(metadata.generationId)
        val documents = generation.collection(COLLECTION_CHUNKS).get().await().documents
            .sortedBy { it.getLong(FIELD_INDEX) }
        require(documents.size == metadata.chunkCount) { "Cloud backup is incomplete" }
        val chunks = documents.map { it.getBlob(FIELD_PAYLOAD)?.toBytes() ?: error("Cloud chunk is missing") }
        val json = CloudChunkCodec.decode(chunks, metadata.hash)
        backupRepository.preview(json)
        return CloudSnapshot(json, metadata.hash)
    }

    private suspend fun deleteGeneration(database: FirebaseFirestore, uid: String, generationId: String) {
        val generation = userRoot(database, uid).collection(COLLECTION_GENERATIONS).document(generationId)
        val chunks = generation.collection(COLLECTION_CHUNKS).get().await().documents
        val batch = database.batch()
        chunks.forEach { batch.delete(it.reference) }
        batch.delete(generation)
        batch.commit().await()
    }

    private fun generationMetadata(payload: ChunkedPayload, summary: BackupSummary) = mapOf(
        FIELD_FORMAT_VERSION to 1,
        FIELD_HASH to payload.hash,
        FIELD_CHUNK_COUNT to payload.chunks.size,
        FIELD_BYTE_COUNT to payload.byteCount,
        FIELD_HABIT_COUNT to summary.habitCount,
        FIELD_HISTORY_COUNT to summary.historyCount,
        FIELD_CONTENT_COUNT to summary.contentCount,
        FIELD_GOAL_COUNT to summary.goalCount,
        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
        FIELD_CLIENT_UPDATED_AT to System.currentTimeMillis(),
    )

    private fun userRoot(database: FirebaseFirestore, uid: String) =
        database.collection(COLLECTION_USERS).document(uid)

    @Suppress("DiscouragedApi")
    private fun serverClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return id.takeIf { it != 0 }?.let(context::getString)?.takeIf(String::isNotBlank)
    }

    private fun updateAccount(auth: FirebaseAuth) {
        val user = auth.currentUser
        mutableAccount.value = if (user == null) {
            CloudAccountState.SignedOut
        } else {
            CloudAccountState.SignedIn(
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                photoUrl = user.photoUrl?.toString(),
            )
        }
    }

    private fun notSignedIn() {
        mutableSyncState.value = CloudSyncState.Error("Google account is not signed in")
    }

    private fun unavailable() {
        mutableSyncState.value = CloudSyncState.Error("Firebase configuration is missing")
    }

    private fun Throwable.safeMessage(): String = localizedMessage?.take(180) ?: javaClass.simpleName

    private data class CloudMetadata(
        val generationId: String,
        val hash: String,
        val chunkCount: Int,
        val summary: BackupSummary,
    )

    private data class CloudSnapshot(val json: String, val hash: String)

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_SYNC = "sync"
        private const val DOCUMENT_CURRENT = "current"
        private const val COLLECTION_GENERATIONS = "syncGenerations"
        private const val COLLECTION_CHUNKS = "chunks"
        private const val FIELD_GENERATION_ID = "generationId"
        private const val FIELD_FORMAT_VERSION = "formatVersion"
        private const val FIELD_HASH = "sha256"
        private const val FIELD_CHUNK_COUNT = "chunkCount"
        private const val FIELD_BYTE_COUNT = "byteCount"
        private const val FIELD_INDEX = "index"
        private const val FIELD_PAYLOAD = "payload"
        private const val FIELD_HABIT_COUNT = "habitCount"
        private const val FIELD_HISTORY_COUNT = "historyCount"
        private const val FIELD_CONTENT_COUNT = "contentCount"
        private const val FIELD_GOAL_COUNT = "goalCount"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_CLIENT_UPDATED_AT = "clientUpdatedAt"
    }
}

internal data class ChunkedPayload(
    val chunks: List<ByteArray>,
    val hash: String,
    val byteCount: Int,
)

internal object CloudChunkCodec {
    private const val CHUNK_SIZE = 180_000
    const val MAX_CHUNKS = 64
    private const val MAX_BYTES = CHUNK_SIZE * MAX_CHUNKS

    fun encode(json: String): ChunkedPayload {
        val bytes = json.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Cloud backup is too large" }
        return ChunkedPayload(bytes.asList().chunked(CHUNK_SIZE).map { it.toByteArray() }, hash(bytes), bytes.size)
    }

    fun decode(chunks: List<ByteArray>, expectedHash: String): String {
        require(chunks.isNotEmpty() && chunks.size <= MAX_CHUNKS) { "Invalid cloud backup chunks" }
        val size = chunks.sumOf(ByteArray::size)
        require(size <= MAX_BYTES) { "Cloud backup is too large" }
        val bytes = ByteArray(size)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(bytes, destinationOffset = offset)
            offset += chunk.size
        }
        require(hash(bytes) == expectedHash) { "Cloud backup integrity check failed" }
        return bytes.toString(Charsets.UTF_8)
    }

    fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { String.format(java.util.Locale.ROOT, "%02x", it) }
}
