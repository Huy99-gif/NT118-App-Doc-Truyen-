package com.nhom7.truyenapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "bookmarks")
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "char_offset")
    private Integer charOffset;

    @Column(name = "note", length = 500)
    private String note;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }
    public Integer getCharOffset() { return charOffset; }
    public void setCharOffset(Integer charOffset) { this.charOffset = charOffset; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
