@Entity
@Table(name = "reports")
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String s3Key;
    private String originalFileName;

    @Lob
    private String ocrText;

    private String summary;

    private Instant createdAt = Instant.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser owner;

    @JsonIgnore
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestResultEntity> testResults;

    // getters & setters...
}
