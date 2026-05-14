package com.educaflow.subsystem.criptografia.db.repo;

import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;

import java.util.List;

public class DispositivoCriptograficoRepository extends AbstractDispositivoCriptograficoRepository {

    public List<DispositivoCriptografico> findAll() {
        return all().fetch();
    }
}
