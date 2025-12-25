package com.sleepysoong.armydiet.data.remote

import com.google.gson.annotations.SerializedName

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

data class MndRow(
    @SerializedName("dates")
    val dates: String?,
    @SerializedName("brst")
    val brst: String?, // 조식
    @SerializedName("lunc")
    val lunc: String?, // 중식
    @SerializedName("dinr")
    val dinr: String?, // 석식
    @SerializedName("adspcfd")
    val adspcfd: String?, // 부식
    @SerializedName("sum_cal")
    val sumCal: String?
)
