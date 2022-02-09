# Streaming
LIVE/VOD Streaming SpringBoot Application. customized red5 opensource
+ netty framework
+ redis

1) VOD (MP4 -> TS)

- MP4 File Reader/ Sampling
- Analyze Video/Audio Frames
- Client Request Video Stream Section (Start point, End point)
- Get Proper Frames between start and end
- Write TS Packet 
  - PAT, PMT, PES (Video H.264 Nal Unit, AU Delimeter | Audio )
- Set Http Header For TS ( ACCESS_CONTROL_..., CONTENT_TYPE: "video/MP2T")
- Write and Flush


2) LIVE (HLS)
[before]
- FFMPEG Encoding Job ( RTSP/RTMP Stream -> M3U8,TS )
- put HLS binary to Redis
From LIVE_Encoder

> Redis Master Server Can push All Data to Replica Redis Server

- Client Request M3U8, TS File
- Get M3U8, TS File From Redis Server ( serialize/deserialize an Object as Binary Data )
- Write and Flush 
