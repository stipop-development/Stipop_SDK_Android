package io.stipop.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.stipop.*
import io.stipop.activity.DetailActivity
import io.stipop.adapter.AllStickerAdapter
import io.stipop.adapter.PackageAdapter
import io.stipop.extend.RecyclerDecoration
import io.stipop.model.SPPackage
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.fragment_all_sticker.*
import org.json.JSONObject
import java.io.IOException

class AllStickerFragment: Fragment() {

    lateinit var myContext: Context

    var packagePage = 2 // 1 Page -> Trending List
    lateinit var packageAdapter: PackageAdapter
    var packageData = ArrayList<SPPackage>()

    lateinit var allStickerAdapter: AllStickerAdapter
    var allStickerData = ArrayList<SPPackage>()

    private var lastItemVisibleFlag = false

    lateinit var packageRV: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        this.myContext = container!!.context

        return inflater.inflate(R.layout.fragment_all_sticker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val headerV = View.inflate(myContext, R.layout.header_all_sticker, null)
        packageRV = headerV.findViewById(R.id.packageRV)

        stickerLV.addHeaderView(headerV)

        clearTextLL.setOnClickListener {
            keywordET.setText("")
        }

        keywordET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                reloadData()
            }
        })

        packageAdapter = PackageAdapter(packageData, myContext)

        val mLayoutManager = LinearLayoutManager(myContext)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL

        packageRV.layoutManager = mLayoutManager
        packageRV.addItemDecoration(RecyclerDecoration(6))
        packageRV.adapter = packageAdapter

        packageAdapter.setOnItemClickListener(object : PackageAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if (position > packageData.size) {
                    return
                }

                val packageObj = packageData[position]

                goDetail(packageObj.packageId)
            }
        })

        if (Config.allStickerType == "B") {
            // B Type
            allStickerAdapter = AllStickerAdapter(myContext, R.layout.item_all_sticker_type_b, allStickerData)
        } else {
            // A Type
            allStickerAdapter = AllStickerAdapter(myContext, R.layout.item_all_sticker_type_a, allStickerData)
        }

        stickerLV.adapter = allStickerAdapter
        stickerLV.setOnScrollListener(object: AbsListView.OnScrollListener {
            override fun onScrollStateChanged(absListView: AbsListView?, scrollState: Int) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && lastItemVisibleFlag) {
                    packagePage += 1
                    loadPackageData(packagePage)
                }
            }

            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                lastItemVisibleFlag = (totalItemCount > 0) && (firstVisibleItem + visibleItemCount >= totalItemCount);
            }

        })
        stickerLV.setOnItemClickListener { adapterView, view, i, l ->
            // position - 1 : addHeaderView 해줬기 때문!
            val position = i - 1
            if (position < 0 && position > allStickerData.size) {
                return@setOnItemClickListener
            }

            val packageObj = allStickerData[position]
            goDetail(packageObj.packageId)
        }

        allStickerAdapter.notifyDataSetChanged()

        loadPackageData(1)

        loadPackageData(packagePage)
    }

    fun goDetail(packageId: Int) {
        val intent = Intent(myContext, DetailActivity::class.java)
        intent.putExtra("packageId", packageId)
        startActivity(intent)
    }

    fun reloadData() {
        loadPackageData(1)

        packagePage = 2
        loadPackageData(packagePage)
    }

    fun loadPackageData(page: Int) {

        val params = JSONObject()
        params.put("userId", Stipop.userId)
        params.put("pageNumber", page)
        params.put("lang", Stipop.lang)
        params.put("countryCode", Stipop.countryCode)
        params.put("limit", 12)
        params.put("q", Utils.getString(keywordET))

        APIClient.get(
            activity as Activity,
            APIClient.APIPath.PACKAGE.rawValue,
            params
        ) { response: JSONObject?, e: IOException? ->

            if (page == 1) {
                packageData.clear()
                packageAdapter.notifyDataSetChanged()
            } else if (page == 2) {
                allStickerData.clear()
                allStickerAdapter.notifyDataSetChanged()
            }

            if (null != response) {

                if (!response.isNull("body")) {
                    val body = response.getJSONObject("body")

                    val packageList = body.getJSONArray("packageList")

                    for (i in 0 until packageList.length()) {
                        val item = packageList.get(i) as JSONObject

                        val spPackage = SPPackage(item)
                        if (page == 1) {
                            packageData.add(spPackage)
                        } else {
                            allStickerData.add(spPackage)
                        }
                    }

                    if (page == 1) {
                        packageAdapter.notifyDataSetChanged()
                    } else {
                        allStickerAdapter.notifyDataSetChanged()
                    }

                }
            }

        }
    }

    fun downloadPackage(packageId: Int) {

        var params = JSONObject()
        params.put("userId", Stipop.userId)
        params.put("isPurchase", "N")

        APIClient.post(activity as Activity, APIClient.APIPath.DOWNLOAD.rawValue + "/$packageId", params) { response: JSONObject?, e: IOException? ->
            println(response)

            if (null != response) {

                val header = response.getJSONObject("header")

                if (Utils.getString(header, "status") == "success") {
                    Toast.makeText(context, "다운로드 완료!", Toast.LENGTH_LONG).show()

                    downloadTV.setBackgroundResource(R.drawable.detail_download_btn_background_disable)
                }

            } else {

            }
        }

    }

}