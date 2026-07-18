package com.dws.isobarfm.adapter.in.web;

import com.dws.isobarfm.adapter.in.web.dto.ApiErrorResponse;
import com.dws.isobarfm.adapter.in.web.dto.BandDetailResponse;
import com.dws.isobarfm.adapter.in.web.dto.BandListResponse;
import com.dws.isobarfm.adapter.in.web.mapper.BandWebMapper;
import com.dws.isobarfm.application.port.in.FindBandsUseCase;
import com.dws.isobarfm.application.port.in.GetBandDetailsUseCase;
import com.dws.isobarfm.domain.model.Band;
import com.dws.isobarfm.domain.model.BandSort;
import com.dws.isobarfm.domain.model.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bands")
@Tag(name = "Bands", description = "Browse and search music bands")
public class BandController {

    private final FindBandsUseCase findBandsUseCase;
    private final GetBandDetailsUseCase getBandDetailsUseCase;
    private final BandWebMapper mapper;

    public BandController(FindBandsUseCase findBandsUseCase,
                          GetBandDetailsUseCase getBandDetailsUseCase,
                          BandWebMapper mapper) {
        this.findBandsUseCase = findBandsUseCase;
        this.getBandDetailsUseCase = getBandDetailsUseCase;
        this.mapper = mapper;
    }

    @Operation(
            summary = "List and search bands",
            description = "Returns a paginated-style list of bands. Supports partial name search, sorting by name or popularity, and direction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of bands (may be empty)"),
            @ApiResponse(responseCode = "400", description = "Invalid sort or direction value",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Provider returned invalid response",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Provider unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<BandListResponse> listBands(
            @Parameter(description = "Partial band name filter (case-insensitive)")
            @RequestParam(required = false) String search,

            @Parameter(description = "Sort field: NAME or POPULARITY", schema = @Schema(allowableValues = {"NAME", "POPULARITY"}))
            @RequestParam(defaultValue = "NAME") String sort,

            @Parameter(description = "Sort direction: ASC or DESC", schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "ASC") String direction,

            @Parameter(description = "Response language (e.g. en, pt-BR, es)", example = "en")
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        BandSort bandSort = parseBandSort(sort);
        SortDirection sortDirection = parseSortDirection(direction);

        List<Band> bands = findBandsUseCase.find(search, bandSort, sortDirection);
        return ResponseEntity.ok(mapper.toListResponse(bands));
    }

    @Operation(
            summary = "Get band details",
            description = "Returns full details for a single band including biography and album IDs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Band details"),
            @ApiResponse(responseCode = "400", description = "Malformed UUID",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Band not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Provider unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BandDetailResponse> getBandDetails(
            @Parameter(description = "Band UUID", example = "00000000-0000-0000-0000-000000000000")
            @PathVariable UUID id,

            @Parameter(description = "Response language (e.g. en, pt-BR, es)", example = "en")
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        Band band = getBandDetailsUseCase.getById(id);
        return ResponseEntity.ok(mapper.toDetail(band));
    }

    private BandSort parseBandSort(String sort) {
        try {
            return BandSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidSortParameterException(sort);
        }
    }

    private SortDirection parseSortDirection(String direction) {
        try {
            return SortDirection.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDirectionParameterException(direction);
        }
    }

    public static class InvalidSortParameterException extends RuntimeException {
        private final String value;

        public InvalidSortParameterException(String value) {
            super("Invalid sort value: " + value);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class InvalidDirectionParameterException extends RuntimeException {
        private final String value;

        public InvalidDirectionParameterException(String value) {
            super("Invalid direction value: " + value);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
