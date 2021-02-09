package co.wangun.cafepos.util

import android.util.Base64
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.R
import co.wangun.cafepos.util.CryptUtils.CryptObj.cipher
import co.wangun.cafepos.util.CryptUtils.CryptObj.keySpec
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class CryptUtils {

    private object CryptObj {
        val password = "${cxt.getString(R.string.app_name)}_${cxt.getString(R.string.app_name)}".take(16)
        val cipher: Cipher = Cipher.getInstance("AES")
        val keySpec = SecretKeySpec(password.toByteArray(),"AES")
    }

    fun encrypt(input: String): String {
        return try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypt = cipher.doFinal(input.toByteArray())
            Base64.encodeToString(encrypt, Base64.DEFAULT)
        } catch (e: Exception) {
            ""
        }
    }

    fun decrypt(input: String): String {
        return try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decrypt = cipher.doFinal(Base64.decode(input, Base64.DEFAULT))
            String(decrypt)
        } catch (e: Exception) {
            ""
        }

    }
}