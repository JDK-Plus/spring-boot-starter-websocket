package plus.jdk.websocket.common;

public interface IFilter<T> {
    boolean valid(T data);
}
