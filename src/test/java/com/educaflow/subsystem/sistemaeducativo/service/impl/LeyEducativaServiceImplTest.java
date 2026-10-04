package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.Repository;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class LeyEducativaServiceImplTest {

    private LeyEducativaServiceImpl service;

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new LeyEducativaServiceImpl(LeyEducativa.class, Mockito.mock(Repository.class));

        // lenient: los caminos sin mensaje no llaman a I18n.
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
    }

    private LeyEducativa ley(String code, String name) {
        LeyEducativa ley = new LeyEducativa();
        ley.setCode(code);
        ley.setName(name);
        return ley;
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_codeYNameNulos_devuelveOptionalVacio() {
        assertTrue(service.validateInsert(ley(null, null)).isEmpty());
    }

    @Test
    void validateInsert_codeYNameDistintosDeAa_devuelveOptionalVacio() {
        assertTrue(service.validateInsert(ley("LOE", "Ley Orgánica")).isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_codeYNameNulos_devuelveOptionalVacio() {
        assertTrue(service.validateUpdate(ley(null, null), ley("LOE", "Ley")).isEmpty());
    }

    @Test
    void validateUpdate_codeYNameDistintosDeBb_devuelveOptionalVacio() {
        assertTrue(service.validateUpdate(ley("LOE", "Ley Orgánica"), ley("bb", "bb")).isEmpty());
    }

    @Test
    void validateUpdate_codeYNameAa_noSeRechazanEnUpdate() {
        assertTrue(service.validateUpdate(ley("aa", "aa"), ley("LOE", "Ley")).isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateRemove                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateRemove_codeYNameNulos_devuelveOptionalVacio() {
        assertTrue(service.validateRemove(ley(null, null)).isEmpty());
    }

    @Test
    void validateRemove_codeYNameDistintosDeCc_devuelveOptionalVacio() {
        assertTrue(service.validateRemove(ley("LOE", "Ley Orgánica")).isEmpty());
    }

    @Test
    void validateRemove_codeYNameAaYBb_noSeRechazanEnRemove() {
        assertTrue(service.validateRemove(ley("aa", "bb")).isEmpty());
    }

}
