package top.mryan2005.sspubot.sspubotbackend.RequestModel.PostList;

import java.util.List;

/**
 * PostList is a request model representing a list of posts with an ID, title, and links to individual posts.
 * It contains:
 * - id: A unique identifier for the post list.
 * - title: The title of the post list.
 * - links: A list of PostLink objects, each containing a title and URL for individual posts.
 */
public class PostList {
    public long id;
    public String title;
    public List<PostLink> items;
}
