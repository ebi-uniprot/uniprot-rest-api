package org.uniprot.api.unisave.repository.domain.impl;

import java.util.Date;

import javax.persistence.*;

import org.uniprot.api.unisave.repository.domain.Load;
import org.uniprot.api.unisave.repository.domain.LoadPhase;
import org.uniprot.api.unisave.repository.domain.Release;

/**
 * Keep track of the loading information in the database.
 *
 * @author wudong
 */
@Entity(name = "Load")
public class LoadImpl implements Load {

    @Id
    @GeneratedValue
    @Column(name = "Load")
    private long loadId;

    @Column(name = "path")
    private String loadingPath;

    @Column(name = "md5")
    private String loadingFileMD5;

    @Column(name = "start")
    @Temporal(TemporalType.TIMESTAMP)
    private Date starttime;

    @Column(name = "finish")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endtime;

    @Column(name = "currentPhase")
    @Enumerated(EnumType.STRING)
    private LoadPhase phase;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "currentStart")
    private Date phaseStart;

    @ManyToOne(optional = false)
    @JoinColumn(name = "Release_Id")
    private ReleaseImpl release;

    @Override
    public String getFilePath() {
        return this.loadingPath;
    }

    public void setFilePath(String path) {
        this.loadingPath = path;
    }

    public void setFileMD5(String md5) {
        this.loadingFileMD5 = md5;
    }

    @Override
    public String getFileMD5() {
        return this.loadingFileMD5;
    }

    @Override
    public Date startTime() {
        return this.starttime;
    }

    @Override
    public Date finishTime() {
        return this.finishTime();
    }

    @Override
    public void startPhase(LoadPhase phase) {
        if (phase == LoadPhase.NotStarted) {
            this.starttime = new Date();
        } else if (phase == LoadPhase.Done) {
            this.endtime = new Date();
        }
        this.phase = phase;
        this.phaseStart = new Date();
    }

    @Override
    public void finishPhase(LoadPhase phase) {
        // TODO
    }

    @Override
    public Release getRelease() {
        return this.release;
    }

    public void setRelease(Release release) {
        this.release = (ReleaseImpl) release;
    }
}
