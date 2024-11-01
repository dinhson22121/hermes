package net.devnguyen.hermes.dto;

public record AccountTask(String accountId, int queueSize) implements Comparable<AccountTask> {

    @Override
    public int compareTo(AccountTask other) {
        return Integer.compare(other.queueSize, this.queueSize); // Ưu tiên user có số lượng sự kiện lớn hơn
    }
}
