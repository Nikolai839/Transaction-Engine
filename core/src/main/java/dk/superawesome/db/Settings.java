package dk.superawesome.db;

import java.util.Map;

public interface Settings {

    <T> T get(String key);

    class Mapped implements Settings {

        private final Map<String, Object> map;

        public Mapped(Map<String, Object> map) {
            this.map = map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String key) {
            return (T) this.map.get(key);
        }
    }
}
