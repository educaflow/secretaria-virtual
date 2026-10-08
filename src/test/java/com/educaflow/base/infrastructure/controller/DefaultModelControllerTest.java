package com.educaflow.base.infrastructure.controller;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultModelControllerTest {

    @Mock
    private ActionRequest actionRequest;

    @Mock
    private ActionResponse actionResponse;

    private final DefaultModelController controller = new DefaultModelController();

    @Test
    void refreshTab_pideRefrescarLaPestanaSinTocarElPopup() {
        controller.refreshTab(actionRequest, actionResponse);

        verify(actionResponse).setSignal("refresh-tab", null);
        verify(actionResponse, never()).setView(any());
        verify(actionResponse, never()).setReload(true);
    }
}
