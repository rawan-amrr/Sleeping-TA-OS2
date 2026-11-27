package core;


public interface StateListener {
    void onUpdate(int working, int sleeping, int waiting, int comeLater);
    void onLog(String message);
}
