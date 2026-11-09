package com.nhom7.truyenapi.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    @OneToMany(mappedBy = "user")
    private List<PasswordResetToken> passwordResetTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Library> libraryEntries = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<ReadingHistory> readingHistories = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Bookmark> bookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Rating> ratings = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<PasswordResetToken> getPasswordResetTokens() { return passwordResetTokens; }
    public void setPasswordResetTokens(List<PasswordResetToken> passwordResetTokens) {
        this.passwordResetTokens = passwordResetTokens;
    }
    public List<Library> getLibraryEntries() { return libraryEntries; }
    public void setLibraryEntries(List<Library> libraryEntries) { this.libraryEntries = libraryEntries; }
    public List<ReadingHistory> getReadingHistories() { return readingHistories; }
    public void setReadingHistories(List<ReadingHistory> readingHistories) {
        this.readingHistories = readingHistories;
    }
    public List<Bookmark> getBookmarks() { return bookmarks; }
    public void setBookmarks(List<Bookmark> bookmarks) { this.bookmarks = bookmarks; }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    public List<Rating> getRatings() { return ratings; }
    public void setRatings(List<Rating> ratings) { this.ratings = ratings; }
}