package com.spot.meet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final Context context;
    private List<Event> eventList;
    private List<Event> fullList;
    private String searchQuery = "";
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    public EventAdapter(Context context, List<Event> eventList, OnItemClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.fullList = new java.util.ArrayList<>(eventList);
        this.listener = listener;
    }

    public void updateList(List<Event> newList, String query) {
        this.eventList = newList;
        this.searchQuery = query;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        
        setHighlightedText(holder.tvTitle, event.title, searchQuery);
        setHighlightedText(holder.tvDesc, event.description, searchQuery);
        
        holder.tvPlaces.setText(event.availablePlaces + " / " + event.totalPlaces + " spots left");
        
        String dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date(event.eventTimestamp));
        holder.tvDateLoc.setVisibility(View.VISIBLE);
        setHighlightedText(holder.tvDateLoc, dateStr + " • " + event.locationName, searchQuery);

        String imageToLoad = (event.thumbnailUrl != null && !event.thumbnailUrl.isEmpty()) 
                ? event.thumbnailUrl : event.mainImageUrl;

        if (imageToLoad != null && !imageToLoad.isEmpty()) {
            if (imageToLoad.contains("ngrok") || imageToLoad.contains("onrender")) {
                com.bumptech.glide.load.model.GlideUrl glideUrl = new com.bumptech.glide.load.model.GlideUrl(
                        imageToLoad,
                        new com.bumptech.glide.load.model.LazyHeaders.Builder()
                                .addHeader("ngrok-skip-browser-warning", "69420")
                                .addHeader("User-Agent", "Mozilla/5.0")
                                .build()
                );
                Glide.with(context)
                        .load(glideUrl)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(holder.ivImage);
            } else {
                Glide.with(context)
                        .load(imageToLoad)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(holder.ivImage);
            }
        } else {
            holder.ivImage.setBackgroundColor(0xFF2A2A4A);
            holder.ivImage.setImageDrawable(null);
        }

        holder.btnMap.setOnClickListener(v -> {
            String geoUri = "http://maps.google.com/maps?q=loc:" + event.lat + "," + event.lng + " (" + event.title + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvDesc, tvPlaces, tvDateLoc;
        Button btnMap;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_event_image);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvDesc = itemView.findViewById(R.id.tv_event_desc);
            tvPlaces = itemView.findViewById(R.id.tv_places);
            tvDateLoc = itemView.findViewById(R.id.tv_event_date_loc);
            btnMap = itemView.findViewById(R.id.btn_view_map);
        }
    }

    private void setHighlightedText(TextView textView, String text, String query) {
        if (text == null) return;
        if (query == null || query.isEmpty()) {
            textView.setText(text);
            return;
        }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        int start = lowerText.indexOf(lowerQuery);
        while (start != -1) {
            int end = start + lowerQuery.length();
            spannable.setSpan(new android.text.style.ForegroundColorSpan(0xFF7C4DFF), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = lowerText.indexOf(lowerQuery, end);
        }
        textView.setText(spannable);
    }
}
