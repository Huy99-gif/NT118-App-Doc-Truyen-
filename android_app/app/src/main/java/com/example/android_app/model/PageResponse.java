package com.example.android_app.model;

import java.util.ArrayList;
import java.util.List;

public class PageResponse<T> {
    public List<T> items = new ArrayList<>();
    public int page;
    public int size;
    public long totalElements;
    public int totalPages;
}
