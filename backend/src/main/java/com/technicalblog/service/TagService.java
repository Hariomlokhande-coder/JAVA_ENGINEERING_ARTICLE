package com.technicalblog.service;

import com.technicalblog.dto.response.TagResponse;
import com.technicalblog.entity.Tag;
import com.technicalblog.mapper.TagMapper;
import com.technicalblog.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TagService {

    private static final int MAX_TAG_LENGTH = 50;

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagService(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> findAll() {
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    /**
     * Turns free text tag names into persisted tags.
     * Names are trimmed, lower cased and de-duplicated, so "Java", " java " and "JAVA" share one row.
     */
    @Transactional
    public Set<Tag> resolveTags(List<String> names) {
        Set<Tag> resolved = new LinkedHashSet<>();
        if (names == null || names.isEmpty()) {
            return resolved;
        }
        for (String rawName : names) {
            String name = normalise(rawName);
            if (name == null) {
                continue;
            }
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
            resolved.add(tag);
        }
        return resolved;
    }

    private String normalise(String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            return null;
        }
        return name.length() > MAX_TAG_LENGTH ? name.substring(0, MAX_TAG_LENGTH) : name;
    }
}
