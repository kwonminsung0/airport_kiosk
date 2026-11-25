package com.example.airport_kiosk.interfacelog.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** INTERFACE_LOG 테이블의 한 행 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceLogVO {

    private Long logId;
    private String interfaceName;
    private String requestSummary;
    private String resultCode;
    private String successYn;
    private long elapsedMs;
    private LocalDateTime createdAt;
}
