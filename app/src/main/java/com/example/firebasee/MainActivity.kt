package com.example.firebasee

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firebasee.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var firebaseIdTokenListener : FirebaseAuth.IdTokenListener
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        Log.d("GoogleSignIn", result.resultCode.toString())
        if (result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private val database = FirebaseDatabase.getInstance()
    private val databaseReference = FirebaseDatabase.getInstance().reference

    private lateinit var postsValueEventListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth=FirebaseAuth.getInstance()

        firebaseIdTokenListener = FirebaseAuth.IdTokenListener {
            if(auth.currentUser==null) {
                Toast.makeText(this,"Log out",Toast.LENGTH_SHORT).show()
                binding.loginBtn.text = "Login"
                binding.name.text= ""
                binding.email.text= ""
            }
        }

        auth.addIdTokenListener(firebaseIdTokenListener)



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this , gso)

        val gUser= auth.currentUser

        val sharedPref = this.getSharedPreferences("login", Context.MODE_PRIVATE)

        val login = sharedPref.getBoolean("login",false)
        if(login){
            binding.loginBtn.text= "Log out"
        }

        autoLogin2()

        binding.loginBtn.setOnClickListener {
            val login = sharedPref.getBoolean("login",false)
            if (!login){
                signInGoogle()
            }
            else{
                auth.signOut()
                googleSignInClient.signOut().addOnCompleteListener(this){
                    //
                }
                this.getSharedPreferences("login", Context.MODE_PRIVATE).edit().putBoolean("login",false).apply()
//                binding.loginBtn.text="Log in"
            }
        }

        binding.addBtn.setOnClickListener {
            addData({}){}
        }

    }

    private fun signInGoogle()= launcher.launch(googleSignInClient.signInIntent)


    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if (account != null){
                updateUI(account)
            }
        }else{
            Toast.makeText(this, task.exception.toString() , Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)
//        saveCredential(account.idToken!!)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                binding.name.text= account.displayName
                binding.email.text= account.email
                binding.image.setImageURI(account.photoUrl)
                binding.loginBtn.text ="Log out"
                this.getSharedPreferences("login", Context.MODE_PRIVATE).edit().putBoolean("login",true).apply()
            }else{
                Log.d("GoogleSignInAccount",it.exception.toString())
                Toast.makeText(this, it.exception.toString() , Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun saveCredential(token: String){
        this.getSharedPreferences("login", Context.MODE_PRIVATE).edit().putString("token",token).apply()
    }

    private fun autoLogin(){
        this.getSharedPreferences("login", Context.MODE_PRIVATE).getString("token",null)?.let { it ->
            val credential = GoogleAuthProvider.getCredential(it , null)
            auth.signInWithCredential(credential).addOnCompleteListener {task->
                if (task.isSuccessful){
                    val user = task.result.user!!
                    binding.name.text= user.displayName
                    binding.email.text= user.email
                    binding.image.setImageURI(user.photoUrl)
                }else{
                    Log.d("GoogleSignInAccount",task.exception.toString())
                    Toast.makeText(this, task.exception.toString() , Toast.LENGTH_SHORT).show()

                }
            }
        }
    }
    private fun autoLogin2(){
        FirebaseAuth.getInstance().currentUser?.let {
            binding.name.text= it.displayName
            binding.email.text= it.email
            binding.image.setImageURI(it.photoUrl)
        }
    }

//    private fun addData(){
//        val data= binding.input.text.toString()
//        val databaseReference= FirebaseDatabase.getInstance().getReference("POST")
//        val post= Post("abc",100, mapOf("xxx" to listOf("a","b","c")))
//
//        databaseReference.setValue(post)
//    }

    fun addData(onSuccessAction: () -> Unit,
                onFailureAction: () -> Unit) {
        //1
        val postsReference = FirebaseDatabase.getInstance().getReference("GET")
        //2
        val key = postsReference.push().key ?: ""
        val post = Post(key,"zzz",100, mapOf("xxx" to listOf("a","b","c")))
        //3
        postsReference.child(key)
            .setValue(post)
            .addOnSuccessListener { onSuccessAction() }
            .addOnFailureListener { onFailureAction() }
    }

    private fun listenForPostsValueChanges() {
        //1
        postsValueEventListener = object : ValueEventListener {
            //2
            override fun onCancelled(databaseError: DatabaseError) {
                /* No op */
            }
            //
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //4
                if (dataSnapshot.exists()) {
                    val posts = dataSnapshot.children.mapNotNull {
                        it.getValue(Post::class.java) }.toList()
                    Log.d("dataSnapshot",posts.toString())

                } else {
                    //5

                }
            }
        }

    }
}

data class Post(val key: String ,val name: String, val age: Int, val data: Map<String, List<String>>)