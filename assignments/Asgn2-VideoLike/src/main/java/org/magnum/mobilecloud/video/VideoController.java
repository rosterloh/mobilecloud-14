package org.magnum.mobilecloud.video;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
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

import com.google.common.collect.Lists;

/**
 * Controller for Video resources
 * 
 * @author richard.osterloh
 *
 */
@Controller
public class VideoController {
	
	private static final String URL_VIDEO = VideoSvcApi.VIDEO_SVC_PATH + "/{id}";
    private static final String URL_VIDEO_LIKE = URL_VIDEO + "/like";
    private static final String URL_VIDEO_UNLIKE = URL_VIDEO +  "/unlike";
    private static final String URL_VIDEO_LIKED_BY = URL_VIDEO + "/likedby";
    private static final String URL_SEARCH_BY_NAME = VideoSvcApi.VIDEO_SVC_PATH + "/search/findByName";
    private static final String URL_SEARCH_BY_DURATION_LESS = VideoSvcApi.VIDEO_SVC_PATH + "/search/findByDurationLessThan";
    
	private static final String PARAMETER_VIDEO_ID = "id";
	
	@Autowired
	private VideoRepository videoRepository;
	
	/**
     * GET /video to get a list of videos
     * 
     * @return Returns the list of videos that have been added to the
     *		   server as JSON
     */
    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
    public @ResponseBody ResponseEntity<Collection<Video>> getVideoList() {
    	
    	Collection<Video> videos = Lists.newArrayList(videoRepository.findAll());
    	
    	return new ResponseEntity<Collection<Video>>(videos, HttpStatus.OK);
    }

    /**
     * GET /video/{id} to get a video with specific id
     * 
     * @param id video identifier
     * @return the video with the given id or {@code 404} if the video is not found.
     */
    @RequestMapping(value=URL_VIDEO, method=RequestMethod.GET)
    public @ResponseBody ResponseEntity<Video> getVideoById(@PathVariable(PARAMETER_VIDEO_ID) long id) {
    	
		if (!videoRepository.exists(id)) {
			return new ResponseEntity<Video>(HttpStatus.NOT_FOUND);
		} 
		
		Video video = videoRepository.findOne(id);
	
		return new ResponseEntity<Video>(video, HttpStatus.OK);
    }
    
    /**
     * POST /video to add a video.
     * 
     * @param v video data to add
     * @return Returns the JSON representation of the Video object that was 
     * 		   stored along with any updates to that object made by the server
     */
    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
    public @ResponseBody ResponseEntity<Video> addVideo(@RequestBody Video v) {
    	
    	videoRepository.save(v);
    	
    	return new ResponseEntity<Video>(v, HttpStatus.OK);
    }
    
	/**
	 * POST /video/{id}/like to like a video. 
	 * 
	 * @param id video identifier
	 * @param p principal of currently authenticated user
	 * @return {@code 200 OK} on success
	 * 		   {@code 404 Not Found} if the video is not found 
	 * 		   {@code 400 Bad Request} if the user has already liked the video.
	 */
	@RequestMapping(value=URL_VIDEO_LIKE, method=RequestMethod.POST)
	public ResponseEntity<Void> likeVideo(@PathVariable(PARAMETER_VIDEO_ID) long id, Principal p) {
		
		if (!videoRepository.exists(id)){
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
		
		String username = p.getName();
		
		Video v = videoRepository.findOne(id);
		Set<String> likesUsernames = v.getLikesUsernames();
		
		// Checks if the user has already liked the video.
		if (likesUsernames.contains(username)) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		
		// keep track of users have liked a video
		likesUsernames.add(username);
		v.setLikesUsernames(likesUsernames);
		v.setLikes(likesUsernames.size());
		videoRepository.save(v);
		
		return new ResponseEntity<Void>(HttpStatus.OK);
	}
	
	/**
	 * POST /video/{id}/unlike to remove a like
	 * 
	 * @param id video identifier
	 * @param p principal of currently authenticated user
	 * @return  {@code 200 OK} on success, 
	 * 			{@code 404 Not Found} if the video is not found, 
	 *			{@code 400 Bad Request} if the user has not previously liked the specified video.
	 */
	@RequestMapping(value=URL_VIDEO_UNLIKE, method=RequestMethod.POST)
	public ResponseEntity<Void> unlikeVideo(@PathVariable(PARAMETER_VIDEO_ID) long id, Principal p) {

		if (!videoRepository.exists(id)){
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
		
		String username = p.getName();
		
		Video v = videoRepository.findOne(id);
		Set<String> likesUsernames = v.getLikesUsernames();
		
		// Checks if the user has already liked the video.
		if (!likesUsernames.contains(username)) {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		
		// remove track of users have unlike a video
		likesUsernames.remove(username);
		v.setLikesUsernames(likesUsernames);
		v.setLikes(likesUsernames.size());
		videoRepository.save(v);
		
		return new ResponseEntity<Void>(HttpStatus.OK);
	}
	
	/**
	 * GET /video/{id}/likedby to get he users that have liked a video
	 * 
	 * @param id video identifier
	 * @return a list of the string usernames of the users that have liked the specified video,
	 *  		if the video is not found, a {@code 4040 Not found} error is generated.
	 */
	@RequestMapping(value=URL_VIDEO_LIKED_BY, method=RequestMethod.GET)
	public @ResponseBody ResponseEntity<Collection<String>> getUsersWhoLikedVideo (@PathVariable(PARAMETER_VIDEO_ID) long id){
		
		if (!videoRepository.exists(id)) {
			return new ResponseEntity<Collection<String>>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<Collection<String>>(
			videoRepository.findOne(id).getLikesUsernames(),
			HttpStatus.OK);	
	}
	
	
	/**
	 * GET /video/search/findByName?title={title} to search videos by title
	 * 
	 * @param title to search by
	 * @return a list of videos whose titles match the given parameter or an empty list if none are found.
	 */
	@RequestMapping(value=URL_SEARCH_BY_NAME, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title) {
		
		return videoRepository.findByName(title);
	}
	
	/**
	 * GET /video/search/findByDurationLessThan?duration={duration} to search videos whose durations are 
	 * less than a input value.
	 * 
	 * @param duration of maximum video length
	 * @return list of videos whose durations are less than the given parameter or an empty list if none are found.
	 */
	@RequestMapping(value=URL_SEARCH_BY_DURATION_LESS, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan(@RequestParam(VideoSvcApi.DURATION_PARAMETER) long duration) {
		
		return videoRepository.findByDurationLessThan(duration);
	}
}
