package com.droidcba.kedditbysteps.features.news.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.droidcba.kedditbysteps.R
import com.droidcba.kedditbysteps.commons.DevotionalAd
import com.droidcba.kedditbysteps.commons.adapter.ViewType
import com.droidcba.kedditbysteps.commons.adapter.ViewTypeDelegateAdapter
import com.droidcba.kedditbysteps.commons.extensions.inflate

class AdDelegateAdapter : ViewTypeDelegateAdapter {
    override fun onCreateViewHolder(parent: ViewGroup) = AdViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType) {
        holder as AdViewHolder
        holder.bind(item as DevotionalAd)
    }

    class AdViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_ad)) {
        fun bind(item: ViewType) = with(itemView) {
            val devotionalAd = item as DevotionalAd
            val adView = devotionalAd.ad
            val adViewGroup = itemView as ViewGroup
            if (adViewGroup.childCount > 0) adViewGroup.removeAllViews()
            if (adView.parent != null) (adView.parent as ViewGroup).removeView(adView)
            adViewGroup.addView(adView)
        }
    }

}
