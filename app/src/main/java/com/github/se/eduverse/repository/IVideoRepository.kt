package com.github.se.eduverse.model.repository

import com.github.se.eduverse.model.Video

interface IVideoRepository {
  suspend fun saveVideo(video: Video): Boolean

  suspend fun updateVideo(videoId: String, video: Video): Boolean

  suspend fun deleteVideo(videoId: String): Boolean

  suspend fun getVideosByOwner(ownerId: String): List<Video>
}
