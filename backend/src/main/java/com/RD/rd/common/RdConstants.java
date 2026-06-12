package com.RD.rd.common;

/**
 * 业务常量集中地
 *
 * <p>所有 ENUM 字符串字面量集中在这里，避免散在业务代码各处。
 * 数据库存的就是 String（V1 SQL 用 ENUM 类型，应用层做校验）。</p>
 *
 * @author Mavis
 */
public final class RdConstants {

    private RdConstants() {}

    // ============== Project 状态 ==============
    public static final class ProjectType {
        public static final String HARDWARE = "HARDWARE";
        public static final String FIRMWARE = "FIRMWARE";
        public static final String SOFTWARE = "SOFTWARE";
        public static final String MIXED = "MIXED";

        private ProjectType() {}
    }

    public static final class ProjectPhase {
        public static final String EVT = "EVT";
        public static final String DVT = "DVT";
        public static final String PVT = "PVT";
        public static final String MP = "MP";

        private ProjectPhase() {}
    }

    public static final class ProjectStatus {
        public static final String PLANNING = "PLANNING";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String ON_HOLD = "ON_HOLD";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";

        private ProjectStatus() {}
    }

    public static final class MilestoneStatus {
        public static final String NOT_STARTED = "NOT_STARTED";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String COMPLETED = "COMPLETED";
        public static final String DELAYED = "DELAYED";

        private MilestoneStatus() {}
    }

    // ============== Equipment 状态 ==============
    public static final class EquipmentCategory {
        public static final String SPECTRUM_ANALYZER = "SPECTRUM_ANALYZER";
        public static final String OSCILLOSCOPE = "OSCILLOSCOPE";
        public static final String SIGNAL_GENERATOR = "SIGNAL_GENERATOR";
        public static final String NETWORK_ANALYZER = "NETWORK_ANALYZER";
        public static final String POWER_METER = "POWER_METER";
        public static final String OTHER = "OTHER";

        private EquipmentCategory() {}
    }

    public static final class EquipmentStatus {
        public static final String AVAILABLE = "AVAILABLE";
        public static final String IN_USE = "IN_USE";
        public static final String MAINTENANCE = "MAINTENANCE";
        public static final String CALIBRATION_OVERDUE = "CALIBRATION_OVERDUE";
        public static final String SCRAPPED = "SCRAPPED";

        private EquipmentStatus() {}
    }

    public static final class ReservationStatus {
        public static final String PENDING = "PENDING";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String IN_USE = "IN_USE";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";
        public static final String NO_SHOW = "NO_SHOW";

        /** 算"冲突"的状态集合（预约占用中） */
        public static final java.util.Set<String> ACTIVE = java.util.Set.of(
                PENDING, CONFIRMED, IN_USE);

        private ReservationStatus() {}
    }
}
