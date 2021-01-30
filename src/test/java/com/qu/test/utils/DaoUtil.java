package com.qu.test.utils;


import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class DaoUtil {
    @ConfigProperty(name = "quarkus.datasource.db-kind")
    String dbkind;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.username")
    String jdbcUser;

    @ConfigProperty(name = "quarkus.datasource.password")
    String jdbcPassword;

    private Jdbi jdbi;

    @PostConstruct
    public void init(){
        this.jdbi = Jdbi.create(jdbcUrl, jdbcUser, jdbcPassword);
    }



    public List<Map<String,?>> runQuery(String sql, Map<String,?> params){
        return jdbi
                .withHandle( h ->
                        h.createQuery(sql)
                                .bindMap(params)
                                .mapToMap()
                                .stream()
                                .collect(toList()));
    }



    public <T> List<T> runQuery(String sql, Class<? extends T> resultType,  Map<String,?> params){
        return jdbi
                .withHandle( h ->
                        h.createQuery(sql)
                                .bindMap(params)
                                .mapToBean(resultType)
                                .stream()
                                .collect(toList()));
    }



    public <T> T getSingleRow(String sql, Class<? extends T> resultType,  Map<String,?> params){
        return jdbi
                .withHandle( h ->
                        h.createQuery(sql)
                                .bindMap(params)
                                .mapToBean(resultType)
                                .findOnly());

    }

}
