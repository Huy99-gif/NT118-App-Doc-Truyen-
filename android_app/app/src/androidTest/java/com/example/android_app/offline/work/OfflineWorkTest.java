package com.example.android_app.offline.work;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class OfflineWorkTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
                new Configuration.Builder().setExecutor(new SynchronousExecutor()).build());
    }

    /** Yêu cầu đồng bộ tới khi một lần đồng bộ đang chờ/chạy không được bị bỏ qua. */
    @Test
    public void enqueueSync_twice_chainsSecondRun() throws Exception {
        OfflineWork.enqueueSync(context, "a@x.com");
        OfflineWork.enqueueSync(context, "a@x.com");

        List<WorkInfo> infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(OfflineWork.syncWorkName("a@x.com")).get();

        assertEquals(2, infos.size());
    }
}
