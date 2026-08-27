package com.example.demo.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/** JSON 文件持久化实现；适合首版演示和本地恢复，不替代生产数据库。 */
public final class FileSupportStateStore extends InMemorySupportStateStore {

    private final Path file;
    private final ObjectMapper objectMapper;

    public FileSupportStateStore(Path file) {
        this.file = Objects.requireNonNull(file, "file 不能为空").toAbsolutePath().normalize();
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        load();
    }

    public Path file() {
        return file;
    }

    @Override
    protected void afterMutation() {
        persist();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<StoredSupportState> restored = objectMapper.readValue(
                    file.toFile(), new TypeReference<>() { }
            );
            restore(restored);
        } catch (IOException | RuntimeException exception) {
            throw new SupportStorageException("读取支持状态失败: " + file, exception);
        }
    }

    private synchronized void persist() {
        Path parent = file.getParent();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(temporary.toFile(), snapshot());
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new SupportStorageException("写入支持状态失败: " + file, exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // 主写入错误已通过 SupportStorageException 报告。
            }
        }
    }
}
