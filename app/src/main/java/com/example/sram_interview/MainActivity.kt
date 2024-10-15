package com.example.sram_interview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.sram_interview.databinding.ActivityMainBinding
import com.google.gson.JsonParser
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callOAuth()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun callOAuth(){
        val intentUri = Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", STRAVA_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:write,read")
            .build()
        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        startActivityForResult(intent,REQUEST_CODE_STRAVA)

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_STRAVA) {

            // comment out once getting actual access token
            val fragment = FirstFragment.newInstance(TEMP_ACCESS_TOKEN)
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, fragment)
                .commit()

            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                val code = uri?.getQueryParameter("code")
                if (code != null ) { getAccessToken(code) }
            }
        }
    }


    fun getAccessToken(authCode: String) {
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("client_id", STRAVA_CLIENT_ID)
            .add("client_secret", STRAVA_CLIENT_SECRET)
            .add("code", authCode)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", STRAVA_REDIRECT_URI)
            .build()

        val request = Request.Builder()
            .url("https://www.strava.com/api/v3/oauth/token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {}
            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val responseBody = response.body?.string()
                    val accessToken = JsonParser().parse(responseBody).getAsJsonObject().get("access_token").getAsString()
                    val fragment = FirstFragment.newInstance(accessToken)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment_content_main, fragment)
                        .commit()
                }
            }
        })
    }

    companion object {
        const val REQUEST_CODE_STRAVA = 1001
        const val STRAVA_CLIENT_ID = "137506"
        const val STRAVA_CLIENT_SECRET = "f1ca728108ba6e174d27640d107fc923538e04bc"
        const val STRAVA_REDIRECT_URI = "https://callback"
        const val TEMP_ACCESS_TOKEN = "caf07e877c4324f43292be9cfc96c9764d087b7b"
    }

}


