package org.magnum.dataup;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {
	
	public static final String DATA_PARAMETER = "data";
	public static final String ID_PARAMETER = "id";
	public static final String VIDEO_SVC_PATH = "/video";	
	public static final String VIDEO_ID_PATH = VIDEO_SVC_PATH + "/{" + ID_PARAMETER + "}";
	public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
	public static final String BASE_URL = "http://localhost:8080";
	
	@Autowired
	private VideoFileManager videoDataMgr;
	
	private static final AtomicLong currentId = new AtomicLong(0L);	
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	
	/**
	 * Generates the URL for the video
	 * 
	 * @param videoId long unique id
	 * @return URL string
	 */
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

	/**
	 * Retrieves the URL base of the server
	 * 
	 * @return server base URL
	 */
 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}
 	
 	/**
 	 * Saves a video with a unique id 
 	 * 
 	 * @param entity Video to save
 	 * @return unique Video
 	 */
 	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}

 	/**
 	 * Set the unique identifier and data URL for the video
 	 * 
 	 * @param entity Video to check
 	 */
	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			// Assign a unique ID
			entity.setId(currentId.incrementAndGet());
			// Assign a data URL
			entity.setDataUrl(getDataUrl(currentId.get()));
		}
	}
	
	/**
	 *  GET /video
	 *  
	 *  @return the current list of videos
	 */
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		//return new ArrayList<Video>(videos.values());
		return videos.values();
	}
	
	/**
	 * Get a video with a specific identifier
	 * 
	 * @param id specific identifier
	 * @return video object or null if doesn't exist
	 */
	@RequestMapping(value=VIDEO_ID_PATH, method=RequestMethod.GET)
	public @ResponseBody Video getVideoById(
			@PathVariable(ID_PARAMETER) long id){
		if (videos.containsKey(id)) {
		    return videos.get(id);
		} else {
			return null;
		}
	}
	
	/** 
	 * POST /video
	 * 
	 * @param v Video object to add
	 * @return the JSON representation of the video added
	 */
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		return save(v);
	}
	
	/**
	 * POST /video/{id}/data
	 * 
	 * @param id specific identifier
	 * @param data binary mpeg data for the video
	 * @return VideoStatus object with state=VideoState.READY
	 * @throws IOException
	 */
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<VideoStatus> addVideoData(
			@PathVariable(ID_PARAMETER) long id,
			@RequestParam MultipartFile data/*,
			HttpServletResponse response*/) throws IOException {
		Video v = getVideoById(id);
		if(v == null){
			return new ResponseEntity<VideoStatus>(HttpStatus.NOT_FOUND);
		}
		//response.sendError(HttpServletResponse.SC_NOT_FOUND);
		videoDataMgr.saveVideoData(v, data.getInputStream());
		
		return new ResponseEntity<VideoStatus>(new VideoStatus(
				VideoStatus.VideoState.READY), HttpStatus.OK);
	}
	
	/**
	 * GET /video/{id}/data
	 * 
	 * @param id specific identifier
	 * @return binary mpeg data (if any) for the video with the given
	 * @throws IOException
	 */
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getVideoData(
			@PathVariable(ID_PARAMETER) long id,
			HttpServletResponse response) throws IOException {
		Video v = getVideoById(id);
		if (v == null) {
		    response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else {
		    response.setContentType(v.getContentType());
		    OutputStream out = response.getOutputStream();
		    videoDataMgr.copyVideoData(v, out);
		}
	}
}
