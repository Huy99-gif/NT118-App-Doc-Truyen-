package com.nhom7.truyenapi.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "stories")
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "featured", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE NOT NULL")
    private boolean featured;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(
            name = "story_categories",
            joinColumns = @JoinColumn(name = "story_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "story")
    private List<Chapter> chapters = new ArrayList<>();

    @OneToMany(mappedBy = "story")
    private List<Library> libraryEntries = new ArrayList<>();

    @OneToMany(mappedBy = "story")
    private List<ReadingHistory> readingHistories = new ArrayList<>();

    @OneToMany(mappedBy = "story")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "story")
    private List<Rating> ratings = new ArrayList<>();

    public Long getId() { return id; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Set<Category> getCategories() { return categories; }
    public void setCategories(Set<Category> categories) { this.categories = categories; }
    public List<Chapter> getChapters() { return chapters; }
    public void setChapters(List<Chapter> chapters) { this.chapters = chapters; }
    public List<Library> getLibraryEntries() { return libraryEntries; }
    public void setLibraryEntries(List<Library> libraryEntries) { this.libraryEntries = libraryEntries; }
    public List<ReadingHistory> getReadingHistories() { return readingHistories; }
    public void setReadingHistories(List<ReadingHistory> readingHistories) {
        this.readingHistories = readingHistories;
    }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    public List<Rating> getRatings() { return ratings; }
    public void setRatings(List<Rating> ratings) { this.ratings = ratings; }

    @PrePersist
    void onCreate() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
