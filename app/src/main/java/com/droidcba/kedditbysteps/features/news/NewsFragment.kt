package com.droidcba.kedditbysteps.features.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.droidcba.kedditbysteps.KedditApp
import com.droidcba.kedditbysteps.R
import com.droidcba.kedditbysteps.commons.*
import com.droidcba.kedditbysteps.commons.extensions.inflate
import com.droidcba.kedditbysteps.features.news.adapter.NewsAdapter
import com.droidcba.kedditbysteps.features.news.adapter.NewsDelegateAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.NativeExpressAdView
import kotlinx.android.synthetic.main.news_fragment.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject

class NewsFragment : RxBaseFragment(), NewsDelegateAdapter.onViewSelectedListener {
    private val ITEMS_PER_AD = 4
    private val TAG = NewsFragment::class.java.simpleName
    private var AD_ID = "ca-app-pub-3940256099942544/1072772517"
    val adSize by lazy{
        val scale = resources.displayMetrics.density
        val adWidth = news_list.width
        AdSize((adWidth / scale).toInt(), 120)
    }

    override fun onItemSelected(url: String?) {
        if (url.isNullOrEmpty()) {
            Snackbar.make(news_list, "No URL assigned to this news", Snackbar.LENGTH_LONG).show()
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    companion object {
        private val KEY_REDDIT_NEWS = "redditNews"
    }

    @Inject lateinit var newsManager: NewsManager
    private var redditNews: RedditNews? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KedditApp.newsComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container?.inflate(R.layout.news_fragment)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        news_list.apply {
            setHasFixedSize(true)
            val linearLayout = LinearLayoutManager(context)
            layoutManager = linearLayout
            clearOnScrollListeners()
            addOnScrollListener(InfiniteScrollListener({ requestNews() }, linearLayout))
        }

        initAdapter()

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_REDDIT_NEWS)) {
            redditNews = savedInstanceState.get(KEY_REDDIT_NEWS) as RedditNews
            (news_list.adapter as NewsAdapter).clearAndAddNews(redditNews!!.news)
        } else {
            requestNews()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val news = (news_list.adapter as NewsAdapter).getNews()
        if (redditNews != null && news.isNotEmpty()) {
            outState.putParcelable(KEY_REDDIT_NEWS, redditNews?.copy(news = news))
        }
    }

    private fun requestNews() {
        /**
         * first time will send empty string for 'after' parameter.
         * Next time we will have redditNews set with the next page to
         * navigate with the 'after' param.
         */
        val subscription = newsManager.getNews(redditNews?.after ?: "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { retrievedNews ->
                            redditNews = retrievedNews
                            loadNewsAndAds(retrievedNews.news)
                        },
                        { e ->
                            Snackbar.make(news_list, e.message ?: "", Snackbar.LENGTH_LONG).show()
                        }
                )
        subscriptions.add(subscription)
    }

    private fun loadNewsAndAds(news: List<RedditNewsItem>) {
        val adapter = news_list.adapter as NewsAdapter
        val size = adapter.itemCount
        adapter.addNews(news)
        addNativeExpressAds(size)
    }

    private fun initAdapter() {
        if (news_list.adapter == null) {
            news_list.adapter = NewsAdapter(this)
        }
    }

    private fun addNativeExpressAds(start: Int) {
        val adapter = news_list.adapter as NewsAdapter
        val size = adapter.itemCount
        for (i in start..size step ITEMS_PER_AD) {
            val adView = NativeExpressAdView(context)
            adView.adSize = adSize
            adView.adUnitId = AD_ID
            val adObject = AdObject(adView)
            adapter.addAd(i, adObject)
        }
        loadNativeExpressAd(start)
    }

    private fun loadNativeExpressAd(index: Int) {
        if (news_list.adapter == null)return
        val adapter = news_list.adapter as NewsAdapter
        if (index >= adapter.itemCount) return

        val item = adapter.items[index]
        when (item) {
            is AdObject -> {
                val adView = item.ad
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        loadNativeExpressAd(index + ITEMS_PER_AD)
                    }

                    override fun onAdFailedToLoad(errorCode: Int) {
                        Log.e(TAG, "The previous Native Express ad failed to load. Attempting to load the next Native Express ad in the items list.")
                        loadNativeExpressAd(index + ITEMS_PER_AD)
                    }
                }
                adView.loadAd(AdRequest.Builder().build())
            }
            else -> loadNativeExpressAd(index + 1)
        }
    }
}

