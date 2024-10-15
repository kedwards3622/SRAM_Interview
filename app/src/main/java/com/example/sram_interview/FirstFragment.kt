package com.example.sram_interview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sram_interview.databinding.FragmentFirstBinding
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    var profileData : AthleteData = AthleteData(null,null,null,null )

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val ARG_DATA: String = "access_token"

        fun newInstance(data: String?): FirstFragment {
            val fragment: FirstFragment = FirstFragment()
            val args = Bundle()
            args.putString(ARG_DATA, data)
            fragment.setArguments(args)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            val accessToken = requireArguments().getString(ARG_DATA)
            if (accessToken != null) {
                fetchAthleteData(accessToken)
                fetchAthleteClubs(accessToken)
            }
        }
    }

    object RetrofitClient {
        private const val BASE_URL = "https://www.strava.com/api/v3/"

        private val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService: StravaApiService = retrofit.create(StravaApiService::class.java)
    }

    interface StravaApiService {
        @GET("athlete")
        fun getAthlete(@Query("access_token") accessToken: String): Call<Athlete>
        @GET("athletes/{id}/stats")
        fun getAthleteStats(@Path("id") athleteId: Int, @Query("access_token") accessToken: String): Call<AthleteStats>
        @GET("athlete/clubs")
        fun getAthleteClubs(@Query("access_token") accessToken: String): Call<List<Club>>
        @GET("clubs/{id}/activities")
        fun getClubActivity(@Path("id") clubId: Int, @Query("access_token") accessToken: String): Call<List<ClubActivity>>
        @GET("clubs/{id}")
        fun getClubPhrase(@Path("id") clubId: Int, @Query("access_token") accessToken: String): Call<ClubPhrase>
    }

    data class AthleteData(
        var athlete : Athlete?,
        var athleteStats : AthleteStats?,
        var club: Club?,
        var clubActivity: ClubActivity?
    )

    data class Athlete(
        val id: Int,
        val username: String,
        val firstname: String,
        val lastname: String,
        val city: String,
        val country: String,
        val profile: String
    )

    fun fetchAthleteData(accessToken: String) {
        val call = RetrofitClient.apiService.getAthlete(accessToken)

        call.enqueue(object : retrofit2.Callback<Athlete> {
            override fun onResponse(call: Call<Athlete>, response: retrofit2.Response<Athlete>) {
                if (response.isSuccessful) {
                    val athlete = response.body()
                    profileData.athlete = athlete
                    binding.name.text = athlete?.firstname + " " + athlete?.lastname
                    binding.cityAnswer.text = athlete?.city+", "+athlete?.country
                    fetchAthleteStats(accessToken, athlete!!.id)
                }
            }

            override fun onFailure(call: Call<Athlete>, t: Throwable) {
                // set all to blank
            }
        })
    }

    data class AthleteStats(
        val all_run_totals: Totals,
        val all_swim_totals: Totals,
        val biggest_ride_distance: Double,
        val all_ride_totals: Totals,
        val biggest_climb_elevation_gain: Double,
    )

    data class Totals(
        val count: Int,
        val distance: Double,
    )

    fun fetchAthleteStats(accessToken: String, id: Int) {
        val call = RetrofitClient.apiService.getAthleteStats(id, accessToken)

        call.enqueue(object : retrofit2.Callback<AthleteStats> {
            override fun onResponse(call: Call<AthleteStats>, response: retrofit2.Response<AthleteStats>) {
                if (response.isSuccessful) {
                    profileData.athleteStats = response.body()
                    var athleteInfo = profileData.athleteStats
                    if  (athleteInfo != null) {
                        binding.runTotalAnswer.text = athleteInfo.all_run_totals.count.toString()
                        binding.swimTotalAnswer.text = athleteInfo.all_swim_totals.count.toString()
                        binding.bikeTotalAnswer.text = athleteInfo.all_ride_totals.count.toString()
                        binding.bikeBiggestAnswer.text = athleteInfo.biggest_ride_distance.toString()
                        binding.climeAnswer.text = athleteInfo.biggest_climb_elevation_gain.toString()
                    }
                }
            }

            override fun onFailure(call: Call<AthleteStats>, t: Throwable) {
            }
        })
    }

    data class Club (
        val id: Int,
        val name: String,
        val city: String,
        val country: String,
        val profile: String,
        val sport_type: String,
        val member_count: Int,
    )

    fun fetchAthleteClubs(accessToken: String) {
        val call = RetrofitClient.apiService.getAthleteClubs(accessToken)

        call.enqueue(object : retrofit2.Callback<List<Club>> {
            override fun onResponse(call: Call<List<Club>>, response: retrofit2.Response<List<Club>>) {
                if (response.isSuccessful) {
                    val clubs = response.body()
                    if (clubs?.isNotEmpty() == true) {
                        profileData.club = clubs.first()
                        val club = profileData.club
                        if (club != null) {
                            binding.clubInfoHeader.text = club.name
                            binding.clubCityAnswer.text = club.city+", "+club.country
                            binding.clubSportAnswer.text = club.sport_type
                            binding.clubCountAnswer.text = club.member_count.toString()
                            fetchClubActivity(accessToken, clubs.first().id)
                            fetchClubPhrase(accessToken, clubs.first().id)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<Club>>, t: Throwable) {
            }
        })
    }

    data class ClubActivity (
        val distance: Double,
        val elapsed_time: Double,
        val type: String,
        val athlete: Athlete
    )

    fun fetchClubActivity(accessToken: String, id: Int) {
        val call = RetrofitClient.apiService.getClubActivity(id, accessToken)

        call.enqueue(object : retrofit2.Callback<List<ClubActivity>> {
            override fun onResponse(call: Call<List<ClubActivity>>, response: retrofit2.Response<List<ClubActivity>>) {
                if (response.isSuccessful) {
                    profileData.clubActivity = response.body()?.first()
                    val clubInfo = profileData.clubActivity
                    if  (clubInfo != null) {
                        binding.clubAthleteAnswer.text = clubInfo.athlete.firstname+" "+clubInfo.athlete.lastname
                        binding.clubDistanceAnswer.text = clubInfo.distance.toString()
                        binding.clubTypeAnswer.text = clubInfo.type
                        binding.clubTimeAnswer.text = clubInfo.elapsed_time.toString()
                    } else {
                        binding.clubInfo.visibility =  View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<List<ClubActivity>>, t: Throwable) {
            }
        })
    }

    data class ClubPhrase (
        val description: String
    )

    fun fetchClubPhrase(accessToken: String, id: Int) {
        val call = RetrofitClient.apiService.getClubPhrase(id, accessToken)

        call.enqueue(object : retrofit2.Callback<ClubPhrase> {
            override fun onResponse(call: Call<ClubPhrase>, response: retrofit2.Response<ClubPhrase>) {
                if (response.isSuccessful) {
                    val clubInfo = response.body()
                    if  (clubInfo != null) {
                        binding.subheadClub.text = clubInfo.description
                    }
                }
            }

            override fun onFailure(call: Call<ClubPhrase>, t: Throwable) {
            }
        })
    }
}