package fr.insapp.insapp.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import fr.insapp.insapp.R
import fr.insapp.insapp.activities.EventActivity
import fr.insapp.insapp.adapters.CommentRecyclerViewAdapter
import fr.insapp.insapp.models.Event
import fr.insapp.insapp.utility.Utils
import kotlinx.android.synthetic.main.activity_event.*
import kotlinx.android.synthetic.main.fragment_event_comments.*
import java.util.*

/**
 * Created by thomas on 25/02/2017.
 */

class CommentsEventFragment : Fragment() {

    private lateinit var adapter: CommentRecyclerViewAdapter

    private var event: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // arguments

        val bundle = arguments
        if (bundle != null) {
            this.event = bundle.getParcelable("event")
        }

        // adapter

        event.let {
            this.adapter = CommentRecyclerViewAdapter(event!!.comments, Glide.with(this), event!!.id)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // edit comment
        comment_event_input?.setupComponent(adapter, event)

        // recycler view

        recyclerview_comments_event?.setHasFixedSize(true)
        recyclerview_comments_event?.isNestedScrollingEnabled = false

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerview_comments_event?.layoutManager = layoutManager

        recyclerview_comments_event?.adapter = adapter

        // retrieve the avatar of the user

        val user = Utils.user

        val id = resources.getIdentifier(Utils.drawableProfileName(user?.promotion, user?.gender), "drawable", context!!.packageName)
        Glide
            .with(context!!)
            .load(id)
            .apply(RequestOptions.circleCropTransform())
            .transition(withCrossFade())
            .into(comment_event_username_avatar)

        // animation on focus

        comment_event_input?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (activity as EventActivity).appbar_event.setExpanded(false, true)
                (activity as EventActivity).fab_participate_event.hideMenu(false)
            } else {
                val atm = Calendar.getInstance().time

                if (event!!.dateEnd.time >= atm.time) {
                    val handler = Handler()
                    handler.postDelayed({
                        val eventActivity = activity as EventActivity?

                        eventActivity?.fab_participate_event?.showMenu(true)
                    }, 500)
                }
            }
        }
    }
}
