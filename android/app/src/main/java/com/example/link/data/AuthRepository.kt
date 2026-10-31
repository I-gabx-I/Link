package com.example.link.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String): String? =
        runAuth { auth.signInWithEmailAndPassword(email.trim(), password).await() }

    suspend fun register(email: String, password: String): String? =
        runAuth { auth.createUserWithEmailAndPassword(email.trim(), password).await() }

    fun signOut() = auth.signOut()

    // Devuelve null si salio bien, o un mensaje de error entendible.
    private suspend fun runAuth(block: suspend () -> Unit): String? =
        try {
            block()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthWeakPasswordException) {
            "La contraseña debe tener al menos 6 caracteres."
        } catch (e: FirebaseAuthUserCollisionException) {
            "Ya existe una cuenta con ese correo."
        } catch (e: FirebaseAuthInvalidUserException) {
            "Correo o contraseña incorrectos."
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            "Revisa el correo y la contraseña."
        } catch (e: Exception) {
            "No pude conectar con el servicio de cuentas. Revisa tu internet."
        }
}

// Convierte un Task de Firebase en una funcion suspend.
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}