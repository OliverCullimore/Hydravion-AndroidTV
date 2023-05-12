package ml.bmlzootown.hydravion.client

data class SyncEvent (
    val event: String? = null,
    val data: Data? = null
)

data class Data (
    val id: String,
    val eventType: String,
    val title: String? = null,
    val message: String? = null,
    val creator: String,
    val channel: String? = null,
    val content: String? = null,
    val icon: String? = null,
    val thumbnail: String? = null,
    val target: Target? = null,
    val foregroundVisible: String? = null,
    val video: Video? = null,
    val post: Post? = null,
    val deliveryDelayRange: ArrayList<Int>? = null,

    val guid: String? = null,
    val text: String? = null,
    val type: String? = null,
    val attachmentOrder: List<String>? = null,
    val metadata: Metadata? = null,
    val releaseDate: Any? = null,
    val likes: Long? = null,
    val dislikes: Long? = null,
    val score: Long? = null,
    val comments: Long? = null,
)

data class Target (
    val url: String,
    val matchScheme: String,
    val match: String,
    val foregroundDiscardOnMatch: Boolean,
    val matchPortion: String
)

data class Video (
    val creator: String,
    val guid: String
)

data class Post (
    val creator: String,
    val guid: String,
    val id: String,
    val text: String,
    val title: String
)

data class Metadata (
    val hasVideo: Boolean,
    val videoCount: Long,
    val videoDuration: Long,
    val hasAudio: Boolean,
    val audioCount: Long,
    val audioDuration: Long,
    val hasPicture: Boolean,
    val pictureCount: Long,
    val hasGallery: Boolean,
    val galleryCount: Long,
    val isFeatured: Boolean
)


