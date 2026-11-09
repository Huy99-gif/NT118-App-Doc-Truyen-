package com.nhom7.truyenapi.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ratings_user_id_story_id",
                columnNames = {"user_id", "story_id"}))
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "stars", nullable = false,
            columnDefinition = "SMALLINT CHECK (stars BETWEEN 1 AND 5)")
    private Short stars;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public Short getStars() { return stars; }
    public void setStars(Short stars) { this.stars = stars; }
}
