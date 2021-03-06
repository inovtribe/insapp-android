package fr.insapp.insapp.activities

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.analytics.FirebaseAnalytics
import fr.insapp.insapp.R
import fr.insapp.insapp.adapters.AssociationRecyclerViewAdapter
import fr.insapp.insapp.adapters.EventRecyclerViewAdapter
import fr.insapp.insapp.adapters.PostRecyclerViewAdapter
import fr.insapp.insapp.adapters.UserRecyclerViewAdapter
import fr.insapp.insapp.http.ServiceGenerator
import fr.insapp.insapp.models.SearchTerms
import fr.insapp.insapp.models.UniversalSearchResults
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * Created by thomas on 11/12/2016.
 */

class SearchActivity : AppCompatActivity() {

    private lateinit var adapterClubs: AssociationRecyclerViewAdapter
    private lateinit var adapterPosts: PostRecyclerViewAdapter
    private lateinit var adapterEvents: EventRecyclerViewAdapter
    private lateinit var adapterUsers: UserRecyclerViewAdapter

    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // query

        val intent = intent
        if (Intent.ACTION_SEARCH == intent.action) {
            this.query = intent.getStringExtra(SearchManager.QUERY)

            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.CONTENT, query)
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
        }

        // toolbar

        setSupportActionBar(toolbar_search)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val requestManager = Glide.with(this)

        // associations recycler view

        recyclerview_search_clubs.setHasFixedSize(true)
        recyclerview_search_clubs.isNestedScrollingEnabled = false

        val layoutManagerClubs = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerview_search_clubs.layoutManager = layoutManagerClubs

        this.adapterClubs = AssociationRecyclerViewAdapter(mutableListOf(), requestManager, false)
        recyclerview_search_clubs.adapter = adapterClubs

        // posts recycler view

        recyclerview_search_posts.setHasFixedSize(true)
        recyclerview_search_posts.isNestedScrollingEnabled = false

        val layoutManagerPosts = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerview_search_posts.layoutManager = layoutManagerPosts

        this.adapterPosts = PostRecyclerViewAdapter(mutableListOf(), requestManager, R.layout.row_post)
        recyclerview_search_posts.adapter = adapterPosts

        // events recycler view

        recyclerview_search_events.setHasFixedSize(true)
        recyclerview_search_events.isNestedScrollingEnabled = false

        val layoutManagerEvents = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerview_search_events.layoutManager = layoutManagerEvents

        this.adapterEvents = EventRecyclerViewAdapter(mutableListOf(), requestManager, false, R.layout.row_event_with_avatars)
        recyclerview_search_events.adapter = adapterEvents

        // users recycler view

        recyclerview_search_users.setHasFixedSize(true)
        recyclerview_search_users.isNestedScrollingEnabled = false

        val layoutManagerUsers = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerview_search_users.layoutManager = layoutManagerUsers

        this.adapterUsers = UserRecyclerViewAdapter(mutableListOf(), requestManager, false)
        recyclerview_search_users.adapter = adapterUsers

        // hide layouts

        search_clubs_layout?.visibility = LinearLayout.GONE
        search_posts_layout?.visibility  = LinearLayout.GONE
        search_events_layout?.visibility  = LinearLayout.GONE
        search_users_layout?.visibility = LinearLayout.GONE
        search_no_result?.visibility = LinearLayout.GONE

        // search

        generateSearchResults()
    }

    private fun generateSearchResults() {
        if (this.query != null) {
            adapterClubs.associations.clear()
            adapterPosts.posts.clear()
            adapterEvents.events.clear()
            adapterUsers.users.clear()

            val call = ServiceGenerator.client.universalSearch(SearchTerms(query))
            call.enqueue(object : Callback<UniversalSearchResults> {
                override fun onResponse(call: Call<UniversalSearchResults>, response: Response<UniversalSearchResults>) {
                    val results = response.body()
                    if (response.isSuccessful && results != null) {
                        val associations = results.associations
                        associations?.let{
                            for (association in associations) {
                                if (association.profilePicture.isNotEmpty() && association.cover.isNotEmpty()) {
                                    adapterClubs.addItem(association)
                                    search_clubs_layout.visibility = LinearLayout.VISIBLE
                                }
                            }
                        }

                        val posts = results.posts
                        posts?.let {
                            for (post in posts) {
                                adapterPosts.addItem(post)
                                search_posts_layout.visibility = LinearLayout.VISIBLE
                            }
                        }

                        val events = results.events
                        events?.let {
                            val atm = Calendar.getInstance().time
                            for (event in events) {
                                if (event.dateEnd.time > atm.time) {
                                    adapterEvents.addItem(event)
                                    search_events_layout.visibility = LinearLayout.VISIBLE
                                }
                            }
                        }

                        val users = results.users
                        users?.let {
                            for (user in users) {
                                adapterUsers.addItem(user)
                                search_users_layout.visibility = LinearLayout.VISIBLE
                            }
                        }

                        if (results.associations != null && results.events != null && results.posts != null && results.users != null){
                            search_no_result?.visibility = LinearLayout.VISIBLE
                        }
                    } else {
                        Toast.makeText(this@SearchActivity, "SearchActivity", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<UniversalSearchResults>, t: Throwable) {
                    Toast.makeText(this@SearchActivity, "SearchActivity", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}