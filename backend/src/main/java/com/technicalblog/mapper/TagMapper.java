package com.technicalblog.mapper;

import com.technicalblog.dto.response.TagResponse;
import com.technicalblog.entity.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
