package com.eigenmusik.sources;

import com.eigenmusik.exceptions.SourceAuthenticationException;
import com.eigenmusik.tracks.Track;
import com.eigenmusik.tracks.TrackStreamUrl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class SourceService {

    private SourceAccountRepository sourceAccountRepository;

    @Autowired
    public SourceService(SourceAccountRepository sourceAccountRepository) {
        this.sourceAccountRepository = sourceAccountRepository;
    }

    public abstract TrackStreamUrl getStreamUrl(Track track);

    public abstract SourceAccount getAccount(String authCode) throws SourceAuthenticationException;

    public abstract List<Track> getTracks(SourceAccount account);

    public abstract String getName();

    public abstract String getAuthUrl();

    public abstract SourceType getType();

    public SourceAccount save(SourceAccount sourceAccount) {
        return sourceAccountRepository.save(sourceAccount);
    }

    public Source getSource()
    {
        return new Source(this.getName(), this.getAuthUrl(), this.getType());
    };
}
