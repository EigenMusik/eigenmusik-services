package com.eigenmusik.sources.dropbox;

import com.dropbox.core.*;
import com.dropbox.core.v1.DbxClientV1;
import com.dropbox.core.v1.DbxEntry;
import com.dropbox.core.v2.DbxClientV2;
import com.eigenmusik.exceptions.SourceAuthenticationException;
import com.eigenmusik.sources.Source;
import com.eigenmusik.sources.SourceAccount;
import com.eigenmusik.sources.SourceAccountRepository;
import com.eigenmusik.sources.SourceType;
import com.eigenmusik.tracks.Track;
import com.eigenmusik.tracks.TrackSource;
import com.eigenmusik.tracks.TrackStreamUrl;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class Dropbox extends Source {

    private static Logger log = Logger.getLogger(Dropbox.class);
    private final DbxRequestConfig config;
    private DropboxUserRepository dropboxUserRepository;

    private DropboxAccessTokenRepository dropboxAccessTokenRepository;
    private DbxWebAuth webAuth;

    @Autowired
    public Dropbox(SourceAccountRepository sourceAccountRepository, DropboxUserRepository dropboxUserRepository, DropboxConfiguration dropboxConfiguration, DropboxAccessTokenRepository dropboxAccessTokenRepository) {
        super(sourceAccountRepository);
        this.dropboxUserRepository = dropboxUserRepository;
        this.dropboxAccessTokenRepository = dropboxAccessTokenRepository;

        DbxAppInfo appInfo = new DbxAppInfo(dropboxConfiguration.getClientId(), dropboxConfiguration.getClientSecret());

        this.config = new DbxRequestConfig(
                "JavaTutorial/1.0", Locale.getDefault().toString());

        DbxSessionStore dbxSessionStore = new DropboxUserSessionStore();

        this.webAuth = new DbxWebAuth(config, appInfo, dropboxConfiguration.getRedirectUrl(), dbxSessionStore);
    }

    @Override
    public TrackStreamUrl getStreamUrl(Track track) {
        DropboxUser dropboxUser = dropboxUserRepository.findOne(track.getTrackSource().getOwner().getUri());
        DropboxAccessToken accessToken = dropboxUser.getAccessToken();

        DbxClientV1 dbxClient = new DbxClientV1(config, accessToken.getAccessToken());
        try {
            return new TrackStreamUrl(dbxClient.createTemporaryDirectUrl(track.getTrackSource().getUri()).url);
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public SourceAccount getAccount(String uri) throws SourceAuthenticationException {
        try {
            List<NameValuePair> paramsList = URLEncodedUtils.parse(new URI(uri), "UTF-8");

            Map<String, String[]> params = new HashMap<>();
            paramsList.forEach(nameValuePair -> params.put(nameValuePair.getName(), new String[]{nameValuePair.getValue()}));

            // Inject our fake csrf token.
            // TODO figure out session with Spring.
            params.put("state", new String[]{DropboxUserSessionStore.fakeCsrfToken});
            DbxAuthFinish authFinish = webAuth.finish(params);
            DbxClientV2 dbxClient = new DbxClientV2(config, authFinish.accessToken);

            DropboxAccessToken dropboxAccessToken = new DropboxAccessToken(authFinish.accessToken);
            DropboxUser dropboxUser = new DropboxUser(dbxClient.users.getCurrentAccount());
            dropboxUser.setAccessToken(dropboxAccessToken);

            dropboxAccessTokenRepository.save(dropboxAccessToken);
            dropboxUserRepository.save(dropboxUser);

            SourceAccount account = new SourceAccount();
            account.setUri(dropboxUser.getId());
            account.setSource(SourceType.DROPBOX);

            return account;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.BadRequestException e) {
            e.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.ProviderException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.NotApprovedException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.CsrfException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.BadStateException e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public List<Track> getTracks(SourceAccount account) {
        DropboxUser dropboxUser = dropboxUserRepository.findOne(account.getUri());
        DropboxAccessToken accessToken = dropboxUser.getAccessToken();

        DbxClientV1 clientv1 = new DbxClientV1(config, accessToken.getAccessToken());

        try {
            return clientv1.searchFileAndFolderNames("/", "mp3").stream().map(mp3 -> mapToTrack(mp3, account)).collect(Collectors.toList());
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Track mapToTrack(DbxEntry dbxEntry, SourceAccount sourceAccount) {
        TrackSource trackSource = new TrackSource();
        trackSource.setUri(dbxEntry.path);
        trackSource.setSource(SourceType.DROPBOX);
        trackSource.setOwner(sourceAccount);

        Track track = new Track();
        track.setName(dbxEntry.path);
        track.setArtist("Drive File");
        track.setTrackSource(trackSource);

        return track;
    }

    @Override
    public String getName() {
        return "Dropbox";
    }

    @Override
    public String getAuthUrl() {
        return webAuth.start();
    }

    public SourceType getType() {
        return SourceType.DROPBOX;
    }
}
