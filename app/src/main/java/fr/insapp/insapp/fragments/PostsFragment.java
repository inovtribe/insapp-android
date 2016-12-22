package fr.insapp.insapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.insapp.insapp.PostActivity;
import fr.insapp.insapp.http.AsyncResponse;
import fr.insapp.insapp.http.HttpGet;
import fr.insapp.insapp.models.Post;
import fr.insapp.insapp.adapters.PostRecyclerViewAdapter;
import fr.insapp.insapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thoma on 27/10/2016.
 */

public class PostsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private int layout;

    private View view;
    private PostRecyclerViewAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // arguments

        final Bundle bundle = getArguments();
        if (bundle != null) {
            this.layout = bundle.getInt("layout", R.layout.row_post_with_avatars);
        }

        // adapter
        this.adapter = new PostRecyclerViewAdapter(getContext(), layout);
        adapter.setOnItemClickListener(new PostRecyclerViewAdapter.OnPostItemClickListener() {
            @Override
            public void onPostItemClick(Post post) {
                getContext().startActivity(new Intent(getContext(), PostActivity.class).putExtra("post", post));
            }
        });

        generatePosts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.fragment_posts, container, false);

        // recycler view

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_posts);
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // swipe refresh layout

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_posts);
        swipeRefreshLayout.setOnRefreshListener(this);

        return view;
    }

    private void generatePosts() {
        HttpGet request = new HttpGet(new AsyncResponse() {

            public void processFinish(String output) {
                if (!output.isEmpty()) {
                    try {
                        JSONArray jsonarray = new JSONArray(output);

                        for (int i = 0; i < jsonarray.length(); i++) {
                            final JSONObject jsonobject = jsonarray.getJSONObject(i);
                            adapter.addItem(new Post(jsonobject));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        request.execute(HttpGet.ROOTPOST + "?token=" + HttpGet.credentials.getSessionToken());

    }

    @Override
    public void onRefresh() {
        Toast.makeText(getContext(), "onRefresh", Toast.LENGTH_SHORT).show();
        swipeRefreshLayout.setRefreshing(false);
    }
}