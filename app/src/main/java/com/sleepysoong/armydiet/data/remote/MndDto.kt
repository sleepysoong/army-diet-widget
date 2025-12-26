package com.sleepysoong.armydiet.data.remote

import com.google.gson.annotations.SerializedName

// Go: MNDApiResponse
data class MndResponse(
    @SerializedName("DS_TB_MNDT_DATEBYMLSVC_7369")
    val service: MndService?
)

data class MndService(
    @SerializedName("list_total_count")
    val listTotalCount: Int,
    @SerializedName("row")
    val rows: List<MndRow>?
)

// Go: json tag와 일치
data class MndRow(
    @SerializedName("dates")
    val dates: String?,
    @SerializedName("brst")
    val brst: String?,
    @SerializedName("lunc")
    val lunc: String?,
    @SerializedName("dinr")
    val dinr: String?,
    @SerializedName("adspcfd")
    val adspcfd: String?,
    @SerializedName("sum_cal")
    val sumCal: String?
)
