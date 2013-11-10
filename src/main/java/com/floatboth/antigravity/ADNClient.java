package com.floatboth.antigravity;

import java.util.List;
import retrofit.Callback;
import retrofit.http.*;
import retrofit.mime.*;
import com.floatboth.antigravity.data.*;

public interface ADNClient {
  @GET("/users/me/files?include_incomplete=0")
  void myFiles(@Query("before_id") String beforeId, Callback<ADNResponse<List<File>>> cb);

  @PUT("/files/{id}")
  void updateFile(@Path("id") String id, @Body File file, Callback<ADNResponse<File>> cb);

  @DELETE("/files/{id}")
  void deleteFile(@Path("id") String id, Callback<ADNResponse<File>> cb);

  @Multipart
  @POST("/files")
  void uploadFile(@Part("content") TypedContent content, @Part("type") TypedString type, @Part("public") TypedString isPublic, Callback<ADNResponse<File>> cb);
}