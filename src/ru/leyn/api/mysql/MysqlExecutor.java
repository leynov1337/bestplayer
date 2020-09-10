package ru.leyn.api.mysql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.leyn.api.ResponseHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Getter
public final class MysqlExecutor {

    public final ExecutorService ASYNC_THREAD_POOL = Executors.newCachedThreadPool();

    private final MysqlConnection connection;

    static MysqlExecutor getExecutor(MysqlConnection connection) {
        return new MysqlExecutor(connection);
    }

    /**
     * Выполнение SQL запроса
     */
    public void execute(boolean async, String sql, Object... elements) {
        Runnable command = () -> {
            connection.refreshConnection();

            try (MysqlStatement mysqlStatement = new MysqlStatement(connection.getConnection(), sql, elements)) {

                mysqlStatement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        if (async) {
            ASYNC_THREAD_POOL.submit(command);
            return;
        }

        command.run();
    }

    /**
     * Выолнение SQL запроса с получением ResultSet
     */
    public <T> T executeQuery(boolean async, String sql, ResponseHandler<T, ResultSet, SQLException> handler, Object... elements) {
        AtomicReference<T> result = new AtomicReference<>();

        Runnable command = () -> {
            connection.refreshConnection();

            try (MysqlStatement mysqlStatement = new MysqlStatement(connection.getConnection(), sql, elements)) {

                result.set(handler.handleResponse(mysqlStatement.getResultSet()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        if (async) {
            ASYNC_THREAD_POOL.submit(command);
            return null;
        }

        command.run();

        return result.get();
    }
}
