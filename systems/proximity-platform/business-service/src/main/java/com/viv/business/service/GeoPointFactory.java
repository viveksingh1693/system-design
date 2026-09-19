package com.viv.business.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public final class GeoPointFactory {

    private static final int WGS84_SRID = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private GeoPointFactory() {
    }

    public static Point create(
            double latitude,
            double longitude) {

        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(longitude, latitude));

        point.setSRID(WGS84_SRID);

        return point;
    }
}