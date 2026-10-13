package com.example.xiaohongshuai.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.xiaohongshuai.entity.Topic;
import com.example.xiaohongshuai.mapper.TopicMapper;
import com.example.xiaohongshuai.service.TopicService;
import org.springframework.stereotype.Service;

@Service
public class TopicServiceImpl implements TopicService {

    private final TopicMapper topicMapper;

    public TopicServiceImpl(TopicMapper topicMapper) {
        this.topicMapper = topicMapper;
    }

    @Override
    public Topic create(String title, String keywords, String audience, String direction) {
        LocalDateTime now = LocalDateTime.now();
        Topic topic = new Topic();
        topic.setTitle(title);
        topic.setKeywords(keywords);
        topic.setAudience(audience);
        topic.setDirection(direction);
        topic.setStatus("TODO");
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        topicMapper.insert(topic);
        return topic;
    }

    @Override
    public List<Topic> list() {
        return topicMapper.selectList(new QueryWrapper<Topic>().orderByDesc("created_at"));
    }

    @Override
    public Topic getRequired(Long id) {
        Topic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw new IllegalArgumentException("Topic not found: " + id);
        }
        return topic;
    }

    @Override
    public void markGenerated(Long id) {
        Topic topic = getRequired(id);
        topic.setStatus("GENERATED");
        topic.setUpdatedAt(LocalDateTime.now());
        topicMapper.updateById(topic);
    }
}
