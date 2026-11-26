package com.woorisseum.core.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.dsl.module

fun sessionStorageModule(context: Context, projectUrl: String) = module {
    single<SessionStorage> { AndroidSessionStorage(context.applicationContext, projectUrl) }
}

/** 암호 키는 Keystore에, 암호문은 백업에서 제외되는 전용 파일에 저장한다. */
private class AndroidSessionStorage(context: Context, projectUrl: String) : SessionStorage {
    private val scope = MessageDigest.getInstance("SHA-256").digest(projectUrl.toByteArray())
        .joinToString("") { "%02x".format(it) }
    private val alias = "woorisseum.session.$scope"
    private val file = File(context.noBackupFilesDir, "session-$scope")
    private val atomicFile = AtomicFile(file)

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (!file.exists() && !File(file.path + ".bak").exists()) return@withContext null
        protect {
            val parts = atomicFile.readFully().toString(Charsets.UTF_8).split(':', limit = 2)
            if (parts.size != 2) throw SessionStorageException()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }
    }

    override suspend fun write(value: String) = withContext(Dispatchers.IO) {
        protect {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
            val stream = atomicFile.startWrite()
            try {
                stream.write(encoded.toByteArray())
                atomicFile.finishWrite(stream)
            } catch (failure: IOException) {
                atomicFile.failWrite(stream)
                throw failure
            }
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        protect { atomicFile.delete() }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private inline fun <T> protect(block: () -> T): T = try {
        block()
    } catch (_: GeneralSecurityException) {
        throw SessionStorageException()
    } catch (_: IOException) {
        throw SessionStorageException()
    } catch (_: IllegalArgumentException) {
        throw SessionStorageException()
    }
}
