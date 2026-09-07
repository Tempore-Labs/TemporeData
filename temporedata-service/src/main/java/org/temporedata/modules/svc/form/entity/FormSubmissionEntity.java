package org.temporedata.modules.svc.form.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*; import java.time.LocalDateTime;

/** Form submission entity storing submitted data for a form. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_form_submission")
public class FormSubmissionEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 32)
    private String id;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createDateTime;

    @Column(length = 32)
    private String formId;

    @Column(columnDefinition = "TEXT")
    private String data;

    @Column(length = 64)
    private String submitterIp;

    @Column(length = 32)
    private String submitTime;
}